{{/*
Helper defines are prefixed `gymapi.` rather than the conventional `<chartname>.` so that moving
templates/ into a shared platform chart is a directory move with no find-and-replace.
*/}}

{{- define "gymapi.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "gymapi.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{- define "gymapi.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Full label set, for every resource. */}}
{{- define "gymapi.labels" -}}
helm.sh/chart: {{ include "gymapi.chart" . }}
{{ include "gymapi.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/component: {{ .Values.component | default "service" }}
app.kubernetes.io/part-of: {{ .Values.partOf | default "gymapi" }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Selector labels only. These end up in an immutable Deployment field, so they must stay stable
across upgrades — never add version or chart here.
*/}}
{{- define "gymapi.selectorLabels" -}}
app.kubernetes.io/name: {{ include "gymapi.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "gymapi.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "gymapi.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{/*
Image reference. A digest wins over a tag when both are set, because a tag alone makes a rollback
ambiguous — CI should set `image.digest`.
*/}}
{{- define "gymapi.image" -}}
{{- if .Values.image.digest -}}
{{- printf "%s@%s" .Values.image.repository .Values.image.digest -}}
{{- else -}}
{{- printf "%s:%s" .Values.image.repository (default .Chart.AppVersion .Values.image.tag) -}}
{{- end -}}
{{- end -}}
