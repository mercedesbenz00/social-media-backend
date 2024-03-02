package models

type Config struct {
	Master                  NodeInfo          `json:"master_info"`
	Node                    NodeInfo          `json:"node_info"`
	EventBus                EventBusInfo      `json:"event_bus"`
	DefaultStorageLocations []StorageLocation `json:"default_storage_locations"`
	CallbackInfo            CallbackInfo      `json:"callback_info"`
}

type EventBusInfo struct {
	IP             string `json:"ip"`
	Port           string `json:"port"`
	ManagementPort string `json:"management_port"`
	UserName       string `json:"user_name"`
	Password       string `json:"password"`
}

type CallbackInfo struct {
	SecurityHeader string `json:"security_header"`
	CallbackURL    string `json:"callback_url"`
}
