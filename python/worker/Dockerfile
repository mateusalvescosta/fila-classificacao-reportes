FROM python:3.12-slim

WORKDIR /app
COPY worker/requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY worker/common_config.py worker/common_api.py worker/llm_classify.py \
     worker/classifier_worker.py worker/high_alert_worker.py worker/standard_worker.py ./
COPY classification_prompt.py ./

# Cada serviço no compose sobrescreve com command: python -u <script>.py
CMD ["python", "-u", "classifier_worker.py"]
