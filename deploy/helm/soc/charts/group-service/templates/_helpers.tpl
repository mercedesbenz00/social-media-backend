{{/*
Expand the name of the chart.
*/}}
{{- define "group-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "group-service.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "group-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "group-service.labels" -}}
helm.sh/chart: {{ include "group-service.chart" . }}
{{ include "group-service.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "group-service.selectorLabels" -}}
app.kubernetes.io/name: {{ include "group-service.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "group-service.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "group-service.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/* 
Postgres connection url
 */}}
{{- define "group-service.postgresURL" -}}
{{ $host := printf "%s-postgres-svc"  .Release.Name }}
{{ if not .Values.postgres.enabled }}
{{ $host = .Values.application.spring.datasource.url | replace "jdbc:postgresql://" "" | replace ":5432/earthlink" "" }}
{{ end }}
{{ printf "jdbc:postgresql://%s:5432/earthlink" $host }}
{{- end }}

{{- define "group-service.rabbitMQURL" -}}
{{ $host := printf "%s-rabbitmq-svc"  .Release.Name }}
{{ if not .Values.global.rabbitmq.enabled }}
{{ $host = .Values.application.social.rabbit.host }}
{{ end }}
{{ print $host }}
{{- end }}

{{/* Zookeeper Connection String */}}
{{- define "group-service.zookeeperConnectString" -}}
{{ $host := printf "%s-zookeeper-svc"  .Release.Name }}
{{ if not .Values.global.zookeeper.enabled }}
{{ $host = .Values.application.spring.cloud.zookeeper.connectString }}
{{ end }}
{{ print $host }}
{{- end }}
