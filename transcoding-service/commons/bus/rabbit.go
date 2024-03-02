package bus

import (
	"log"
	"strconv"

	"bitbucket.org/creativeadvtech/social-media-backend/transcoding-service/commons/models"
	rabbithole "github.com/michaelklishin/rabbit-hole"
	"github.com/streadway/amqp"
)

type Rabbit struct {
	conn          *amqp.Connection
	management    *rabbithole.Client
	Info          models.EventBusInfo
	prefetchCount int
}

func NewRabbit(info models.EventBusInfo, prefetchCount int) *Rabbit {
	return &Rabbit{Info: info, prefetchCount: prefetchCount}
}

func (r *Rabbit) Init() error {
	var err error
	connectionString := "amqp://" + r.Info.UserName + ":" + r.Info.Password + "@" + r.Info.IP + ":" + r.Info.Port + "/"
	r.conn, err = amqp.Dial(connectionString)

	if err != nil {
		log.Println("ERROR: Failed to connect to RabbitMQ :" + err.Error())
		return err
	}

	log.Println("LOG: RabbitMQ is up")

	r.management, err = rabbithole.NewClient("http://"+r.Info.IP+":"+r.Info.ManagementPort, r.Info.UserName, r.Info.Password)
	if err != nil {
		log.Println("ERROR: Failed to connect to RabbitMQ (management)")
		return err
	}

	log.Println("LOG: RabbitMQ management is up")
	return nil
}

func (r *Rabbit) GetNodesNumber() (int, error) {
	queue, err := r.management.GetQueue("/", models.TRANSCODE_CHUNK_REQUEST.Name)
	if err != nil {
		return -1, err
	}
	log.Println("LOG: The number of nodes is " + strconv.Itoa(queue.Consumers))
	return queue.Consumers, nil
}

func (r *Rabbit) SendData(queue models.QueueInfo, data []byte) error {
	ch, err := r.conn.Channel()
	if err != nil {
		log.Println("ERROR: Failed to open a channel")
		return err
	}
	defer ch.Close()

	q, err := r.createQueueIfNotFound(ch, queue.Name)
	if err != nil {
		log.Printf("ERROR: Failed to declare a queue - %s", queue.Name)
		return err
	}

	err = ch.Publish(
		"",     // exchange
		q.Name, // routing key
		false,  // mandatory
		false,  // immediate
		amqp.Publishing{
			ContentType: "text/plain",
			Body:        data,
		})

	if err != nil {
		log.Println("ERROR: Failed to publish a message")
		return err
	}

	return nil
}

func (r *Rabbit) ReceiveData(queue models.QueueInfo, handler HandleMessage) error {
	ch, err := r.conn.Channel()
	if err != nil {
		log.Println("ERROR: Failed to open a channel")
		return err
	}
	defer ch.Close()
	q, err := r.createQueueIfNotFound(ch, queue.Name)

	if err != nil {
		log.Println("ERROR: Failed to declare a queue")
		return err
	}

	if err = ch.Qos(r.prefetchCount, 0, false); err != nil {
		return err
	}

	msgs, err := ch.Consume(
		q.Name,        // queue
		"",            // consumer
		queue.AutoAck, // auto-ack
		false,         // exclusive
		false,         // no-local
		false,         // no-wait
		nil,           // args
	)

	if err != nil {
		log.Println("ERROR: Failed to register a consumer")
		return err
	}

	forever := make(chan bool)

	go func() {
		for d := range msgs {
			handler(d)
		}
	}()

	log.Printf("LOG: [*] Waiting for messages on " + queue.Name + ". To exit press CTRL+C")
	<-forever
	return nil
}

func (r *Rabbit) createQueueIfNotFound(ch *amqp.Channel, queueName string) (amqp.Queue, error) {
	q, err := ch.QueueDeclare(
		queueName, // name
		true,      // durable
		false,     // delete when unused
		false,     // exclusive
		false,     // no-wait
		nil,       // arguments
	)
	return q, err
}

func (r *Rabbit) Ack() {

}

type HandleMessage func(b amqp.Delivery)

type Bus interface {
	Init() error
	SendData(queue models.QueueInfo, data []byte) error
	ReceiveData(queue models.QueueInfo, handler HandleMessage) error
	GetNodesNumber() (int, error)
	Ack()
}
