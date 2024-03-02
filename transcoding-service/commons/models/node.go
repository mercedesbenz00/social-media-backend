package models

type NodeInfo struct {
	TempFolder     string         `json:"temp_folder"`
	StorageSecrets StorageSecrets `json:"storage_secrets"`
	DbConfig       DbConfig       `json:"db_config"`
}

type DbConfig struct {
	Dialect          string `json:"dialect"`
	ConnectionString string `json:"connection_string"`
}
