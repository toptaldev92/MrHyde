import errno
from sqlite3 import Error as SQLError
import logging
from time import time
from shutil import rmtree
from os.path import isdir, join, dirname, realpath
from os import makedirs, chdir, getcwd
from configparser import ParsingError as ConfigError
import threading
import shutil

import git

from bottle import template

import DbHandler
import JekyllManager
import FileManager
import RepoUtils

OWN_PATH = dirname(realpath(__file__))


class RepositoryManager:
    __db = None
    __fm = None
    __utils = None
    __jm = None
    __base_dir = ""
    __git = git.Git()

    __logger = logging.getLogger(__name__)

    def __init__(self):
        from main import cm

        try:
            self.__base_dir = cm.get_base_dir()
            self.__base_url = cm.get_base_url()
            self.__db = DbHandler.DbHandler(cm.get_db_file())
            self.__fm = FileManager.FileManager()
            self.__utils = RepoUtils.RepoUtils()
            self.__jm = JekyllManager.JekyllManager()
        except SQLError:
            raise
        except ConfigError:
            raise

    def utils(self):
        return self.__utils

    def db(self):
        return self.__db

    def fm(self):
        return self.__fm

    def jm(self):
        return self.__jm

    def init_repository(self, url, diff):
        from main import cm

        id = self.utils().generateId(cm.get_hash_size())
        repo_path = join(self.__base_dir, id)
        deploy_path = ''.join([cm.get_deploy_base_path(), id, cm.get_deploy_append_path()])

        if self.utils().repository_exists(id):
            return None

        self.db().insertData('repo', id, repo_path, deploy_path, url, int(time()), 1)
        shutil.copytree(OWN_PATH + '/redirector/', deploy_path)

        t = threading.Thread(target=self.init_repository_async, args=(id, deploy_path, repo_path, url, diff))
        t.daemon = True
        t.start()

        return (id, ''.join(['http://', id, '.', cm.get_base_url(), '/poller.html']))

    def init_repository_async(self, id, deploy_path, repo_path, url, diff):
        if not isdir(self.__base_dir):
            makedirs(self.__base_dir, 0o755, True)
        try:
            self.__git.clone(url, repo_path)
            if diff is not None and diff is not '':
                self.apply_diff(id, repo_path, diff)
            self.log().info('Repository cloned to ' + repo_path + '.')
            # deploy_path = self.fm().setup_deployment(id)
            self.jm().build(repo_path, deploy_path)

        except OSError as exception:
            if exception.errno == errno.EPERM:
                self.log().error("Permission to " + repo_path + " denied.")
                self.deploy_error_page(deploy_path, 'I/O error',
                                       "An I/O error occurred, stay calm and wait for a technician!")
                with open(deploy_path+'/statuscode.txt', 'w') as outfile:
                    outfile.write(str(0))
        except SQLError as exception:
            self.log().error(exception.__str__())
            self.deploy_error_page(deploy_path, 'Database error',
                                   "We encountered an error in our database. (╯°□°）╯︵ ┻━┻")
            with open(deploy_path+'/statuscode.txt', 'w') as outfile:
                outfile.write(str(0))
        except git.GitCommandError as exception:
            self.log().error(exception.__str__())
            self.deploy_error_page(deploy_path, 'VCS error',
                                   "We're having problems applying your changes. Have you been sacrificing some branches to the gods of git lately?")
            with open(deploy_path+'/statuscode.txt', 'w') as outfile:
                outfile.write(str(0))

    def list_repositories(self):
        dir_list = [[f['path'].split('/')[-1], f['url']] for f in self.db().list('repo') if isdir(f['path'])]
        return dir_list

    def delete_repository(self, id):
        try:
            repo = self.db().list('repo', '', "id='%s'" % id)[0]
            rmtree(repo['path'])
            self.db().deleteData('repo', "id='%s'" % repo['id'])
        except OSError as exception:
            if exception.errno == errno.ENOENT:
                self.log().error('Repository ' + id + ' not found.')
                raise
            elif exception.errno == errno.EPERM:
                self.log().error('Insufficient permissions to remove repository ' + id + '.')
                raise
        except SQLError:
            raise

    # TODO update repos async
    def update_repository(self, id, diff):
        try:
            repo_path = self.db().list('repo', 'path', "id='%s'" % id)[0]
            deploy_path = self.db().list('repo', 'deploy_path', "id='%s'" % id)[0]
            diff_file = self.fm().create_diff_file(id, diff)
            self.apply_diff(id, repo_path, diff_file)
            build_successful = self.jm().build(repo_path, deploy_path)
            if build_successful:
                return (id, ''.join(['https://', id, '.', self.__base_url]))
            else:
                return (id, self.jm().get_errors())
        except OSError as exception:
            if exception.errno == errno.ENOENT:
                self.log().error('Repository ' + repo_path + ' not found.')
                raise
            elif exception.errno == errno.EPERM:
                self.log().error('Insufficient permissions to remove repository ' + repo_path + '.')
                raise
        except SQLError:
            self.log().error('Database error.')
            raise
        except git.GitCommandError as exception:
            self.log().error(exception.__str__())
            raise

    def log(self):
        return self.__logger

    def apply_diff(self, id, repo_path, diff):
        try:
            # repo_path = self.db().list('repo', 'path', "id='%s'" % id)[0]
            old_dir = getcwd()
            chdir(repo_path)
            diff_file = self.fm().create_diff_file(id, diff)
            self.__git.apply(diff_file)
            # self.utils().update_timestamp(id)
        except SQLError:
            raise
        except git.GitCommandError:
            self.log().error('Unable to apply diff.')
            raise
        except OSError:
            raise
        finally:
            chdir(old_dir)

    def deploy_error_page(self, deploy_path, error_type, error_msg):
        if not isdir(deploy_path):
            makedirs(deploy_path, 0o755, True)
        index_file_path = join(deploy_path, 'index.html')
        index_file = open(index_file_path, 'w')
        index_file.write(template('list_view', rows=[error_msg], header=error_type))
        index_file.close()
