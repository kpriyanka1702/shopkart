{{- define "shopkart.labels" -}}
app.kubernetes.io/part-of: shopkart
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end -}}
