package com.nextiva.config

class TrunkBased implements BranchingModel {
    public static final Branch trunk = new Branch("trunk", /^master$/)
}