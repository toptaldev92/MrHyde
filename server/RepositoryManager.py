import git
import errno
import ConfigManager
import logging

from os.path import isdir, join
from os import makedirs, chdir, listdir, getcwd


class RepositoryManager:
    __cm = None
    __base_dir = ""
    __git = git.Git()

    __logger = logging.getLogger(__name__)

    def __init__(self):
        try:
            self.__cm = ConfigManager.ConfigManager()
            self.__base_dir = self.__cm.get_base_dir()
        except KeyError:
            raise

    def init_repository(self, url):
        if not self.repository_exists(url):
            if isdir(self.__base_dir):
                try:
                    old_dir = getcwd()
                    chdir(self.__base_dir)
                    self.__git.clone(url)
                    self.__logger.info('Repository cloned.')
                    chdir(old_dir)
                    return join(self.__base_dir, self.get_repo_dir(url))
                except OSError as exception:
                    if exception.errno == errno.EPERM:
                        self.__logger.error("Permission to " + self.get_repo_dir(url) + " denied.")
                        raise
            else:
                try:
                    old_dir = getcwd()
                    makedirs(self.__base_dir, 0o755, True)
                    chdir(self.__base_dir)
                    self.__git.clone(url)
                    self.__logger.info('Repository cloned.')
                    chdir(old_dir)
                    return join(self.__base_dir, self.get_repo_dir(url))
                except OSError as exception:
                    if exception.errno == errno.EPERM:
                        self.__logger.error("Permission to " + self.get_repo_dir(url) + " denied.")
                    raise
        else:
            return join(self.__base_dir, self.get_repo_dir(url))

    def repository_exists(self, url):
        repo_name = url.split('/')[-1]
        dir_name = repo_name.split('.')[0]

        if isdir(join(self.__base_dir, dir_name)):
            return True
        else:
            return False

    def list_repositories(self):
        dir_list = [f for f in listdir(self.__base_dir) if isdir(join(self.__base_dir, f))]
        return dir_list

    def list_single_repository(self, repo_name):
        repo_dir = join(self.__base_dir, repo_name)
        if isdir(repo_dir):
            file_list = [f for f in listdir(repo_dir)]
            return file_list
        else:
            return None

    def get_repo_name(self, url):
        return url.split('/')[-1]

    def get_repo_dir(self, url):
        return self.get_repo_name(url).split('.')[0]

    def cleanup_repositories(self):
        pass

    def update_repository(self, id):
        pass
