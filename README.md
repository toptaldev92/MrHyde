# MrHyde
Avoid turning yourself into a raging monster by using our Android app to ease blogging using Jekyll and GitHub pages.


## Server
The MrHyde Server gets requests from the clients, builds the page and returns a link to the built page to the clients, so they can preview it.
The server is mainly written in Python. It requires some setup to run and currently only works on systems that have the ability to create subdomains by just creating a new folder.

### Basic idea behind how the server works:
Server gets a request from a client, clones the repo and applies a diff, then calls jekyll on this repo.
The Client meanwhile gets the link to a dart/js script, that polls for the console output of the jekyll instance and the status code of the jekyll instance.
As soon as the status code is 0, the script will redirect to the jekyll html output.

### Setup

#### Requirements

 * Python
 * git
 * ruby and [jekyll](http://jekyllrb.com/) (Jekyll needs to be in $PATH!)
 * [GitPython](https://github.com/gitpython-developers/GitPython) 
 * [bottle](http://bottlepy.org/docs/dev/index.html) 
 
 #### Configuration
 
 There is a config_example.ini in the server root folder. Copy/Rename it to config.ini and open and edit it:
 * [default]path = # Path where the repos are going to be stored
 * [default]deploy_path = # Path where the built page is going to be deployed from
 * [default]deploy_path_append = # This string is going to be appended to the deploy_path after the id. So it's deploy_path+"/"+id+deploy_path_append
 * [default]url = # the URL the server is going to run on. Should just stay the way it is, unless you want to edit the code aswell
 * [default]client_secret = # A secret the client has to know aswell, so you can make sure that only your clients use your server
 * [database]path = # the path and name of the database
 * [repo]id_length = <the length of the id that gets prepended to the deploy_path_append>
