import static com.nextiva.SharedJobsStaticVars.*


def call(String extraPath='.', String deployEnvironment=null, String pythonName='python3', String venvDir=VENV_DIR) {
    dir(extraPath) {
        pythonUtils.createVirtualEnv(pythonName, venvDir)
        pythonUtils.venvSh("""
            pip install -U wheel
            python setup.py sdist bdist bdist_egg bdist_wheel
        """, false, venvDir)

        //log.deprecated('Publishing pypi-package to the old repo - pypi.nextiva.xyz')
        // pythonUtils.venvSh("""
        //     pip install devpi-client
        //     devpi use http://pypi.nextiva.xyz
        //     devpi login root --password iampythonian
        //     devpi use root/dev
        //     devpi upload --from-dir dist
        // """, false, venvDir)

        log.info('Publishing pypi-package to Nexus')

        if ( deployEnvironment ) {
            pythonUtils.venvSh("""
                twine upload --config-file /etc/nexus_pypi_config_${deployEnvironment} dist/*
            """, false, venvDir)
        }
        else {
            pythonUtils.venvSh("""
                twine upload --config-file /etc/nexus_pypi_config_dev dist/*
                twine upload --config-file /etc/nexus_pypi_config_production dist/*
            """, false, venvDir)
        }
    }
}
