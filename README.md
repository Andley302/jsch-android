
# JSch Android - VPN Tunnel via SSH

O **JSch Android** é um projeto que visa criar um túnel VPN utilizando a biblioteca JSch para SSH, com encaminhamento de porta (port forwarding) e suporte ao `pdnsd` e `tun2socks`, além de integrar o serviço de VPN do Android. O objetivo é oferecer uma alternativa ao **Trilead SSH2**, que é comumente utilizado em aplicativos de VPN baseados em SSH. Embora o JSch seja mais simples e menos atualizado, ele oferece estabilidade e desempenho competitivo, com foco em eficiência no uso de memória.

## Características

- **Túnel VPN SSH**: Criação de um túnel VPN via SSH, com encaminhamento de porta para trafegar pacotes.
- **Suporte a Payloads HTTP**: Possui suporte à injeção de payloads HTTP, tanto via proxy HTTP quanto SSL, através de um socket local que serve de intermediário para o SSH.
- **Uso eficiente de memória**: O serviço de conexão SSH funciona em uma thread separada, o que reduz o uso de RAM e melhora a velocidade, mas com possíveis instabilidades.
- **Reconexão automática**: O serviço monitora a rede e tenta reconectar automaticamente após desconexões de rede, como quedas de Wi-Fi.
- **Bypass VPN**: O "Bypass" inicia o serviço `tunfd` do Android ao iniciar a conexão, removendo o app do builder VPN para facilitar a resolução de hosts. Se a conexão for bem-sucedida, o túnel é atualizado com os dados corretos.
- **Importe ou exporte configurações**: O app conta com um sistema simples para exportar e importar configurações no formato json, permitindo que você teste em outros locais ou compartilhe com terceiros sem a necessidade de digitar tudo manualmente. Obs.: O json não conta com nenhum tipo de criptografia.

## Comparação de Desempenho

Aqui está uma comparação entre o desempenho do **Trilead SSH2** e do **JSch Android**:

### Trilead SSH2

- **Tempo de Conexão (iniciar):** 1.95s
- **Tempo de Reconexão (desconectar e reconectar Wi-Fi após 5s):** 12.91s
- **Tempo de Reconexão (desligar e ligar Wi-Fi rapidamente):** 8.63s

**Speedtest:**
- **Ping:** 44ms
- **Download:** 41.3 Mbps
- **Upload:** 22.0 Mbps
- **Jitter:** 13ms

### JSch Android

- **Tempo de Conexão (iniciar):** 2.96s
- **Tempo de Reconexão (desconectar e reconectar Wi-Fi após 5s):** 13.55s
- **Tempo de Reconexão (desligar e ligar Wi-Fi rapidamente):** 9.75s

**Speedtest:**
- **Ping:** 37ms
- **Download:** 35.3 Mbps
- **Upload:** 27.5 Mbps
- **Jitter:** 27ms

## Arquitetura

### Conexão SSH

O serviço de conexão SSH é executado em um thread separado, o que proporciona uma experiência mais fluida com menor uso de memória. O uso de memória para conexões com payload direto (sem injeção personalizada) gira em torno de **50MB**, enquanto conexões com injeção personalizada podem consumir até **100MB** de RAM, dependendo da configuração.

### Reconexão Automática

Ao perder a conexão, o JSch Android cria um **timer** para verificar o status da rede. Assim que a conexão é restabelecida, ele tenta reconectar automaticamente, o que pode ser uma vantagem sobre o Trilead SSH2, que não implementa um mecanismo de reconexão tão eficiente.

### Bypass VPN

Ao iniciar a conexão, o serviço `tunfd` do Android é ativado, o que permite a criação do túnel VPN. Durante esse processo, o app é removido do **VPN Builder** para possibilitar a resolução de hosts corretamente. Se a conexão for bem-sucedida, o `tunfd` é atualizado com os dados corretos da conexão SSH.

## Como Usar

### 1. Configuração do Projeto

1. Clone o repositório:
   ```bash
   git clone https://github.com/Andley302/jsch-android.git
   ```
   
2. Abra o projeto no Android Studio e faça as configurações necessárias para o seu dispositivo.

3. Instale as dependências necessárias no arquivo `build.gradle`.

### 2. Conectar a um Servidor SSH

Através do aplicativo, insira os dados do servidor SSH (host, porta, usuário, senha) e clique em "Start". O aplicativo tentará se conectar ao servidor SSH e criar o túnel VPN. Certifique-se que o servidor e o proxy estejam configurados corretamente para aceitar as conexões.

### 3. Suporte a Payloads

Caso deseje usar injeção de payload HTTP, configure o proxy HTTP/SSL nas opções do aplicativo. O app irá usar um socket local para passar os dados através do túnel SSH.

### 4. Reconexão Automática

O app monitorará sua conexão e tentará reconectar automaticamente caso a rede seja perdida.

### 5. Bypass VPN

A funcionalidade de "Bypass" é ativada automaticamente ao iniciar a conexão. Isso inicializa o serviço `tunfd` do Android e permite uma maior flexibilidade na resolução de hosts.

## Contribuindo

Se você deseja contribuir para o projeto, siga os passos abaixo:

1. Faça um **fork** do repositório.
2. Crie uma nova branch para sua feature (`git checkout -b feature/minha-feature`).
3. Faça as alterações e comite suas mudanças (`git commit -am 'Adiciona nova feature'`).
4. Envie sua branch para o repositório remoto (`git push origin feature/minha-feature`).
5. Abra um **pull request**.

## Licença

Este projeto é licenciado sob a **MIT License** - veja o arquivo [LICENSE](LICENSE) para mais detalhes.
