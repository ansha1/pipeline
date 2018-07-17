def call(){
    def changeSet = currentBuild.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
    def entries = changeLogSets[i].items
        Set authors = [];
        if (changeSet != null) {
            for (change in changeSet.items) {
            print(change.getClass())
            authors.add(getUserEmail { user = change.author })
            }
        }
    }
    log.info("Commit authors:")
    authors.each { log.info("  - ${it}") }
    return authors
}