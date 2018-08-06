def call(){
    def changeSets = currentBuild.changeSets
    Set authors = [];
    changeSets.each{
        if (it) {
            for (change in it.items) {
                authors.add(getUserEmail { user = change.author })
            }
        }
    }

    log.info("Commit authors:")
    authors.each { log.info("  - ${it}") }
    return authors
}