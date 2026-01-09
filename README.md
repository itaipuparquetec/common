# Apresentação
Este projeto é uma biblioteca common para várias aplicações Java WEB.

## Construção 🚧
- Sempre que fizer uma alteração, altere a versão no pom.xml, caso contrário o deploy dará erro.
  - Utilize versionamento semântico para isso (saiba mais em https://semver.org/lang/pt-BR/). o padrão semver para este versionamento Execute o arquivo ```api/src/main/resources/container/docker-compose.yml``` via ```docker compose up```;
- As credenciais do banco de dados, email, e authorization server estão aqui no [Sharepoint do SYSHUB](https://itaipuparquetec-my.sharepoint.com/:f:/r/personal/emanuel_fonseca_itaipuparquetec_org_br/Documents/F%C3%A1brica%20de%20Software?csf=1&web=1&e=Y1rMgY);
    - Você deverá inserí-las como variáveis de ambiente na sua estação local.
      - versão Maior(MAJOR): quando fizer mudanças incompatíveis na API,
      - versão Menor(MINOR): quando adicionar funcionalidades mantendo compatibilidade, e
      - versão de Correção(PATCH): quando corrigir falhas mantendo compatibilidade.Pode fazer isso através da PATH do seu sistema operacional, ou pela própria IDE (no caso do Jetbrains IDEA a opção é Environment Variables).
- Após o deploy, suba a versão no projeto cliente que utilizará a nova funcionalidade.

## Testes 🚀
- Se o coverage diminuir a biblioteca também quebrará na pipeline.

Enjoy 😎