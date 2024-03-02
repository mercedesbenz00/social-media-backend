FROM 310830963532.dkr.ecr.eu-central-1.amazonaws.com/synapse:1694605122

RUN apt update && apt upgrade -y && apt install -y git


RUN pip install git+https://github.com/joshuaboniface/matrix-synapse-rest-password-provider

RUN pip install git+https://github.com/matrix-org/synapse-s3-storage-provider



