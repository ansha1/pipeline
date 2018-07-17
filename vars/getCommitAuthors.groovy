def call(){
    def changeSet = currentBuild.changeSetsz
    Set authors = [];
    if (changeSet != null) {
        for (change in changeSet.items) {
            authors.add(getUserEmail { user = change.author })
        }
    }
    log.info("Commit authors:")
    authors.each { log.info("  - ${it}") }
    return authors
}