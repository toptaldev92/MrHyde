#!/usr/bin/env python

import errno
import logging
import json

from bottle import request, Bottle, run, abort, template

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
            return template('list_view', rows=['No repositories available.'], header="Available repositories:")
        else:
            return template('list_view', rows=repos, header="Available repositories:")

@jekyll_server.post('/jekyll')
def create_repository():
    try:
        url = request.POST.get('url')
        diff = request.POST.get('diff')
        return rm.init_repository(url, diff)
    except OSError as exception:
        if exception.errno == errno.EPERM:
            abort(403, 'Permission denied.')
    except KeyError:
        abort(500, 'Unable to parse config file.')

@jekyll_server.get('/jekyll/<repo_name:path>')
def show_repository(repo_name):
    files = rm.list_single_repository(repo_name)
    if files is not None:
        if request.content_type == 'application/json':
            return json.dumps(files)
        else:
            if len(files) < 1:
                return template('repo_overview', rows=[['Empty repository.', 'http://github.com/s1hofmann'], ['test', 'hi!']], header="Repository content:")
            else:
                return template('list_view', rows=files, header="Repository content:")
    else:
        abort(404, 'Repository not found!')

run(jekyll_server, host='127.0.0.1', port=8787)
