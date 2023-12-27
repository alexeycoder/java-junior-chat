## Урок 5. Клиент/Сервер своими руками

1. Если в начале сообщения есть '@4'&nbsp;&mdash; то значит отсылаем сообщение клиенту с идентификатором 4.

2. Если в начале сообщения нет '@'&nbsp;&mdash; значит, это сообщение нужно послать остальным клиентам.

3. *Добавить админское подключение, которое может кикать других клиентов.

    1. При подключении оно посылает спец. сообщение, подтверждающее, что это&nbsp;&mdash; админ.

    2. Теперь, если админ посылает сообщение kick 4&nbsp;&mdash; то отключаем клиента с идентификатором 4.

4. **Подумать, как лучше структурировать программу (раскидать код по классам).

### Решение:

Сервер &mdash; модуль [junior-chat-server/](junior-chat-server/)

Клиент &mdash; модуль [junior-chat-client/](junior-chat-client/)

*Скомпилировать модули (рабочая - общая директория мульти-модульного проекта):*

	mvn clean package

*Запустить сервер (рабочая - директория ./junior-chat-server/):*

	java -jar target/junior-chat-server-0.0.1-SNAPSHOT-jar-with-dependencies.jar

*Запустить клиента (рабочая - директория ./junior-chat-client/):*

	java -jar target/junior-chat-client-0.0.1-SNAPSHOT-jar-with-dependencies.jar

### Пример:

![Пример](https://github.com/alexeycoder/illustrations/blob/main/java-junior-chat-client-server/example.png?raw=true)
