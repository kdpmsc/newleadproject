FROM ollama/ollama:latest

COPY start-ollama.sh /start-ollama.sh
RUN sed -i 's/\r$//' /start-ollama.sh && chmod +x /start-ollama.sh

ENTRYPOINT ["/start-ollama.sh"]