Checkers Client/Server Project

Structure:
- CheckersServer : run first
- CheckersClient : open one or two clients after the server is running

Requirements:
- JDK 11+ (tested for Java 11 compatibility)
- Maven for easiest run path in IntelliJ or terminal

Terminal run:
1) Server
   cd CheckersServer
   mvn clean compile
   mvn exec:java

2) Client
   cd CheckersClient
   mvn clean compile
   mvn exec:java

Gameplay:
- Create account or log in
- Queue for a 2-player online match
- Red moves first
- Forced captures are enforced
- Multi-jumps are enforced
- Kings move both directions
- Win when opponent has no legal moves or no pieces
- Draw after a long quiet sequence
- Rematch supported after game over

Persistence:
- Accounts and stats are saved in server-data/accounts.ser on the server side

Notes:
- The client also includes offline bot play for local testing.
