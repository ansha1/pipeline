package utils

class BranchNames {
    public static final Set<String> feature = ['feature/foobar', 'feature', 'foobar', 'release', 'hotfix', 'hrelease', 'f-dev', 'dev-',
                                               'development', 'aaaarelease/123', 'd-release/10.0.0', '-release/foobar']
    public static final Set<String> release = ['release/123', 'release/1.0.0', 'release/0.0.0', 'release/0.0.0-SNAPSHOT',
                                               'hotfix/123', 'hotfix/1.0.0', 'hotfix/0.0.0', 'hotfix/0.0.0-SNAPSHOT']
    public static final Set<String> develop = ['dev', 'develop']
    public static final Set<String> master = ['master']
}