import static com.nextiva.SharedJobsStaticVars.*


def call(String extraPath='.', String deployEnvironment=null, String pythonName='python3') {
    dir(extraPath) {
        pythonUtils.createVirtualEnv(pythonName)
        pythonUtils.venvSh("""
            pip install -U wheel
            python setup.py sdist bdist bdist_egg bdist_wheel
        """)

        log.warning('DEPRECATED: publishing pypi-package to old repo - pypi.nextiva.xyz')
        pythonUtils.venvSh("""
            pip install -U devpi-client
            devpi use http://pypi.nextiva.xyz
            devpi login root --password iampythonian
            devpi use root/dev
            devpi upload --from-dir dist
        """)

        log.info('Publishing pypi-package to Nexus')

        if ( deployEnvironment ) {
            pythonUtils.venvSh("""
                twine upload --config-file /etc/nexus_pypi_config_${deployEnvironment} dist/*
            """)
        }
        else {
            pythonUtils.venvSh("""
                twine upload --config-file /etc/nexus_pypi_config_dev dist/*
                twine upload --config-file /etc/nexus_pypi_config_production dist/*
            """)
        }
    }
}
