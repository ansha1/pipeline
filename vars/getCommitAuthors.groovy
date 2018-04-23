def call(){
    def changeSet = currentBuild.changeSets[0];
    Set authors = [];
    if (changeSet != null) {
        for (change in changeSet.items) {
            authors.add(getUserEmail { user = change.author })
        }
    }
    print("Commit authors:")
    authors.each { print(it) }
    return authors
}