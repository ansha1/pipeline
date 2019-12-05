package com.nextiva.config

import java.util.regex.Pattern

class Branch {
    String name
    Pattern branchPattern

    Branch(String name, String branchPattern) {
        this.name = name
        this.branchPattern = Pattern.compile(branchPattern)
    }

    Branch(String name, Pattern branchPattern) {
        this.name = name
        this.branchPattern = branchPattern
    }
}