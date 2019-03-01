workflow "Main" {
  on = "push"
  resolves = ["build"]
}

action "build" {
  uses = "LucaFeger/action-maven-cli@9d8f23af091bd6f5f0c05c942630939b6e53ce44"
  runs = "mvn clean install"
}
