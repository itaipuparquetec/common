FROM postgres:18-alpine

# Define a porta padrão interna do Postgres
ENV PGPORT=5433

# Opcional: documenta a porta correta
EXPOSE 5433

# Sobrescreve o comando padrão para garantir a porta
CMD ["postgres", "-p", "5433"]