#!/usr/bin/env python

import errno
import logging
import json

from git import GitCommandError
from sqlite3 import Error as SQLError
from configparser import Error as ConfigError

from bottle import request, Bottle, run, abort, template, static_file
import sys

import RequirementsChecker
import ConfigManager
import FileManager
import RepoUtils
import RepositoryManager

logging.basicConfig(filename='jekyll_server.log', level=logging.DEBUG)
logger = logging.getLogger(__name__)

if len(sys.argv) < 2:
    logger.error('Config file missing!\nUsage: %s <config_file>' % sys.argv[0])
    exit('Config file missing!\nUsage: %s <config_file>' % sys.argv[0])
else:
    config_file = sys.argv[1]

try:
    cm = ConfigManager.ConfigManager(config_file)
    fm = FileManager.FileManager()
    utils = RepoUtils.RepoUtils()
    rm = RepositoryManager.RepositoryManager()
except ConfigError:
    exit('Unable to parse config file.')
except SQLError:
    exit('Error connecting to database.')

RequirementsChecker.check_requirements(logger)

jekyll_server = Bottle()

@jekyll_server.get('/jekyll/')
def list_all_repositories():
    return template('list_view', rows=['Your friendly Jekyll blogging client for Android.'], header='Welcome to Mr. Hyde!')
    # repos = rm.list_repositories()
    #
    # if request.content_type.startswith('application/json'):
    #     return json.dumps(repos)
    # else:
    #     if len(repos) < 1:
    #         return template('list_view', rows=['No repositories available.'], header='Available repositories:')
    #     else:
    #         return template('repo_overview', rows=repos, header='Available repositories:')

@jekyll_server.post('/jekyll/')
def create_repository():
    try:
        if request.content_type.startswith('application/json'):
            url = request.json.get('gitCheckoutUrl')
            diff = request.json.get('gitDiff')
            client_secret = request.json.get('clientSecret')
            if client_secret != cm.get_client_secret():
                abort(400, 'Bad request')
            (repo_id, repo_url) = rm.init_repository(url, diff)
            expires = utils.get_expiration_date(repo_id)
            return json.dumps({'previewUrl': repo_url, 'previewExpirationDate': expires, 'previewId': repo_id})
        else:
            url = request.POST.get('gitCheckoutUrl')
            diff = request.POST.get('gitDiff')
            client_secret = request.POST.get('clientSecret')
            if client_secret != cm.get_client_secret():
                abort(400, 'Bad request')
            (repo_id, repo_url) = rm.init_repository(url, diff)
            if repo_url is not None and isinstance(repo_url, str):
                return template('list_view', rows=[repo_url, repo_id], header='Your new repository is available at:')
            elif isinstance(repo_url, list):
                return template('list_view', rows=repo_url, header='Build failed.')
            else:
                abort(500, 'Internal error. Sorry for that!')
    except OSError as exception:
        if exception.errno == errno.EPERM:
            abort(403, 'Permission denied.')
    except KeyError:
        abort(500, 'Internal error. Sorry for that!')
    except GitCommandError:
        abort(400, 'Bad request')
    except IOError:
        abort(500, 'Internal error. Sorry for that!')

@jekyll_server.get('/jekyll/<id:path>/')
def show_repository(id):
    files = fm.list_directory(id)
    if files is not None:
        if request.content_type.startswith('application/json'):
            return json.dumps(files)
        else:
            if len(files) < 1:
                return template('repo_overview', rows=[['Empty repository.', '']], header="Repository content:")
            else:
                return template('list_view', rows=files, header="Repository content:")
    else:
        abort(404, 'Repository not found!')

@jekyll_server.get('/jekyll/<id:path>/<static_path>')
def download_file(id, static_path):
    try:
        file = fm.file_download(id, static_path)
        if file is True:
            return static_file('/'.join([id, static_path]), root=cm.get_base_dir(), download=True)
    except OSError as exception:
        if exception.errno == errno.ENOENT:
            abort(404, 'File not found.')
        elif exception.errno == errno.EPERM:
            abort(403, 'Permission denied.')

@jekyll_server.delete('/jekyll/<id:path>/')
def delete_repository(id):
    try:
        rm.delete_repository(id)
    except OSError as exception:
        if exception.errno == errno.ENOENT:
            abort(404, 'Repository not found.')
        elif exception.errno == errno.EPERM:
            abort(403, 'Permission denied.')
    except SQLError:
        abort(500, 'Internal error. Sorry for that!')

@jekyll_server.put('/jekyll/<id:path>/')
def update_repository(id):
    try:
        if request.content_type.startswith('application/json'):
            diff = request.json.get('gitDiff')
            client_secret = request.json.get('clientSecret')
            if client_secret != cm.get_client_secret():
                abort(400, 'Bad request')
            repo_url = rm.update_repository(id, diff)
            expires = utils.get_expiration_date(id)
            return json.dumps({'previewUrl': repo_url, 'previewExpirationDate': expires, 'previewId': id})
        else:
            diff = request.POST.get('gitDiff')
            client_secret = request.POST.get('clientSecret')
            if client_secret != cm.get_client_secret():
                abort(400, 'Bad request')
            url = rm.update_repository(id, diff)
            return template('list_view', rows=[url], header='Repository updated.')
    except OSError as exception:
        if exception.errno == errno.ENOENT:
            abort(404, 'Repository not found.')
        elif exception.errno == errno.EPERM:
            abort(403, 'Permission denied.')
    except GitCommandError:
        abort(500, 'Failed to apply patch.')
    except SQLError:
        abort(500, 'Internal error. Sorry for that!')

run(jekyll_server, host='127.0.0.1', port=8787, debug=False)
