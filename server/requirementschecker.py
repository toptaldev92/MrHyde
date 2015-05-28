from subprocess import Popen, PIPE

def check_requirements(logger, logger_only=False, errors_only=True):
    cmds = [('git', ['git', '--version']), ('ruby', ['ruby', '--version']), 
            ('jekyll', ['jekyll', '--version'])]
    for item in cmds:
        cmd = item[1]
        name = item[0]
        if not logger_only and not errors_only:
            print('Checking', name, 'requirement: ', end="")
        logger.info('Checking '+name+ ' requirement: ')
        try:
            process = Popen(cmd, stdout=PIPE)
            (output,err) = process.communicate()
            exit_code = process.wait()
            if exit_code == 0:
                if not logger_only and not errors_only:
                    print('Found with version string:', output)
                logger.info('Found with version string: '+str(output))
            else:
                raise FileNotFoundError()
        except FileNotFoundError:
            if not logger_only:
                print('[Error]', name, 'not found. Please install it (and/or add it to your PATH)!')
            logger.error(name + ' not found. Please install it (and/or add it to your PATH)!')