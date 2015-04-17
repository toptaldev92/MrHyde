#!/usr/bin/env python

import errno
import logging
import json

from bottle import request, Bottle, run, abort, template, static_file

import RepositoryManager
import RequirementsChecker

logging.basicConfig(filename='jekyll_server.log', level=logging.DEBUG)
logger = logging.getLogger(__name__)

rm = RepositoryManager.RepositoryManager()

RequirementsChecker.check_requirements(logger)

jekyll_server = Bottle()

@jekyll_server.get('/jekyll')
def list_all_repositories():
    repos = rm.list_repositories()

    if request.content_type == 'application/json':
        return json.dumps(repos)
    else:
        if len(repos) < 1:
            return template('list_view', rows=['No repositories available.'], header='Available repositories:')
        else:
            return template('repo_overview', rows=repos, header='Available repositories:')

@jekyll_server.post('/jekyll')
def create_repository():
    try:
        if request.content_type == 'application/json':
            url = request.json.get('url')
            diff = request.json.get('diff')
            repo_url = rm.init_repository(url, diff)
            return json.dumps(repo_url)
        else:
            url = request.POST.get('url')
            diff = request.POST.get('diff')
            repo_url = rm.init_repository(url, diff)
            return template('list_view', rows=[repo_url], header='Your new repository is available at:')
    except OSError as exception:
        if exception.errno == errno.EPERM:
            abort(403, 'Permission denied.')
    except KeyError:
        abort(500, 'Unable to parse config file.')


@jekyll_server.get('/jekyll/<repo_name:path>/__page/<static_path>')
def show_jekyll_output(repo_name, static_path):
    return static_file(repo_name+'/__page/'+static_path, root=rm.get_config().get_base_dir())


@jekyll_server.get('/jekyll/<id:path>')
def show_repository(id):
    files = rm.list_single_repository(id)
    if files is not None:
        if request.content_type == 'application/json':
            return json.dumps(files)
        else:
            if len(files) < 1:
                return template('repo_overview', rows=[['Empty repository.', '']], header="Repository content:")
            else:
                return template('list_view', rows=files, header="Repository content:")
    else:
        abort(404, 'Repository not found!')

@jekyll_server.delete('/jekyll/<id:path>')
def delete_repository(id):
    try:
        rm.delete_repository(id)
    except OSError as exception:
        if exception.errno == errno.ENOENT:
            abort(404, 'Repository not found.')
        elif exception.errno == errno.EPERM:
            abort(403, 'Permission denied.')

run(jekyll_server, host='127.0.0.1', port=8787, debug=True)
