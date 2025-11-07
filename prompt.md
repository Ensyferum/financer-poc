Você é um engenheiro de software responsável por desenvolver um sistema de gestão de finanças conforme caracteristicas abaixo. Utilize das ferramentas mais modernas e eficazes para o desenvolvimento do sistema. Se houver ferramentas melhores para alguma funcionalidade, sugira para que possamos analisar qual deve ser a utilizada:
Backend:
    O sistema vai ser orientado a serviços e eventos
    Todo o backend vai ser desenvolvido com Java 21, RestFul e GrapQL. Utilizar as melhores práticas das ferramentas Spring.
    Usaremos microserviço como base de arquitetura do projeto, então vamos focar em aplicações muito leves pra trabalhar melhor com réplicas
    Iremos organizar os serviços por dominio e pra execuções mais intensas, faremos serviços especializados
    Todo o sistema deve ter teste unitário implementado utilizando JUNIT5 com AssertThat
    Também teremos serviços de orquestração que utilizarão Linguagem funcional para a chamada dos serviços necessários. Eles devem ter somente a lógica funcional da transação, mas a regra de negócio será de cada serviço especializado.
    Os serviços serão feitos com Maven e utilizaremos Git com Gitflow pra organizar o/os repositórios.
    Faremos desenhos técnicos utilizando mermaid para explicar cada funcionalidade nos serviços de orquestração
    Todas as APIs deverão estar expostas com Swagger-UI
    O Banco de dados será um Postgres
    O projeto deve ser estruturado respeitando as camadas
    A aplicação deve ter logs padronizados em todas as execuções importante. O log deve estar categorizada com o dominio + função + etapa + descrição
    
    
FrontEnd:
    O sistema terá um frontend feito em Angular.
    Para cada funcionalidade criada no backend, deveremos ter uma tela no frontend capaz de visualizar as informações referente ao serviço criado.

Arquitetura:
    Utilizaremos Docker com DockerCompose para estruturação do sistema. 
    Teremos o MongoDB como banco NOSQL e o Postgres como SQL
    Usaremos o Kafka para mensageria
    Kafka, Postres, todos os serviços e demais ferramentas deverão estar no Docker
    A configuração de replicas, memoria, cpu, etc., deverá ser gerida por um arquivo IAC separado.
    Todas as filas do Kafka deverão ser geridas por um IAC separado
    Toda a configuração de banco de dados deverá ser gerida por um IAC separado
    Devemos ter um controle de versão das tabelas e da carga de dados, versionado, para poder remontar o ambiente sempre que necessário
    Para o dominio de Solicitação, em específico, teremos um serviço a parte utilizando CAMUNDA que fará todo o control

Testes:
    Criaremos alguns fluxos utilizando ROBOT pra fazer os testes funcionais da aplicação
    O Robot deve ser estruturado com as melhores práticas de organização de projeto Python corporativo

Inicialmente vamos rodar o projeto totalmente local e vamos construir por etapas. Organize as etapas de um modo que todas sejam testáveis antes de seguirmos para as próximas.


Funcional:
    Objetivos: 
        O sistema deve armazenar informações financeiras de uma pessoa física, sendo elas compras no cartão de crédito, PIX, boletos bancários, DOC/TED. 
        Devemos ter um controle dinamico de Contas Bancárias, Cartões de Créditos, Cartões de Débito, Faturas (frequentes e exporádicas).
        O sistema deve manter um controle de balanço de todas as contas com visões unificadas e separadas também.
        Deve ser possível criar uma transação bancária de maneira fácil. Também de visualiza-la, modifica-la ou exclui-la.
        As exclusões deve ser todas virtuais, ou seja, iremos inativar o registro mas não exclui-lo.
        Devemos manter um histórico de todas as alterações que um registro teve.
        Pra cada transação deve ser criado um registro de solicitação com controle de estado para sabermos de o registro foi concluido, está em andamento, está com erro ou se nem foi criado.
        