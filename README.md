
<h1 align="center">
  Shortz <img width=25 src="https://github.com/PauloRPA/Shortz/blob/master/src/main/resources/static/images/shortz_icon.png">
</h1>

<p align="center">Um simples encurtador de urls.</p>
<p align="center"><em> ⚠️ Em desenvolvimento! ⚠️ </em></p>

<!--toc:start-->

- [Instalação](#Instalação)
- [Configuração](#Configuração)
- [Funcionalidades](#Funcionalidades)

<!--toc:end-->

## Instalação

1. Clone o repositório git para sua maquina.
```
git clone https://github.com/PauloRPA/Shortz
```
2. Entre na pasta do repositório e rode o comando `mvn spring-boot:run`.
3. Espere o processo de build e após o seu fim a aplicação estará disponível na porta `9999`.

Caso o maven não esteja instalado na sua maquina, é possivel usar o wrapper 'mvnw' disponível na pasta root do repositório.
Use então: `./mvnw spring-boot:run` ou `./mvnw.bat spring-boot:run` caso esteja usando Windows.

## Configuração

Devido a aplicação ainda estar em desenvolvimento, por hora, está disponível apenas um banco de dados em memória (H2).
A aplicação também tem apenas um perfil (@Profile) no momento, sendo este, o de desenvolvimento, chamado 'dev'.

### Variáveis de ambiente

- `SHORTZ_DEFAULT_USER`: (String) Define o nome de usuário 'admin' padrão da aplicação. Na sua ausência o usuario 'dev' é usado. Aceita também o valor `generate` para que um usuário aleatório seja gerado no momento que a aplicação for iniciada.
- `SHORTZ_DEFAULT_PASSWORD`: (String) Define a senha do usuário 'admin' padrão da aplicação. Na sua ausência uma senha aleatória é gerada no momento que a aplicação for iniciada.
- `INITIAL_SLUG_LENGTH`: (Integer) Tamanho do slug a ser gerado caso o usuário não insira nada ao criar uma nova 'ShortUrl'. (O slug é o ultimo trecho de uma url, neste caso, o slug é a 'url encurtada')
- `SLUG_DICTIONARY`: (String) Caracteres a serem utilizados para geração de uma 'Shorturl'.  
- `SUPPORTED_PROTOCOLS` (CSV, String) Lista de valores separados por vírgula com os protocolos aceitos para criação de uma url curta.

## Funcionalidades

Essa é uma aplicação simples que permite com que o usuário encurte uma url para que posteriormente possa acessar a url original por meio de sua versão encurtada. A url encurtada pode ser arbitrária ou gerada pela aplicação.
Um usuário com permisões de admin pode ativar, desativar, limitar a criação de urls curtas e altarar informações de outros usuários do sistema.
Essa aplicação foi idealizada para ser usada em ambiente 'domestico' algo como um homelab com poucos usuários, portanto tenha em mente que, apesar de funcional, esta aplicação possui limitações de desempenho, segurança e escalabilidade que ja foram previstos durante seu processo de desenvolvimento.
<hr>


