<!doctype html>
<html>
    <head>
    <meta charset="utf-8">
    <title>MrHyde - a Jekyll scratchpad</title>
    <link rel="stylesheet" href="http://faudroid.markab.uberspace.de/jekyll/static/layout.css" type="text/css">
    <link href='http://fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'>
    </head>

    <body>
        <p><strong>Available repositories:</strong></p>
        <table>
        %for row in rows:
            <tr>
                <td>{{row[0]}}</td><td><a href='{{row[1]}}'>View on GitHub</a></td>
            </tr>
        %end
        </table>
        <div align="center"><img src="http://faudroid.markab.uberspace.de/jekyll/static/ic_background.svg"></div>
        <p></p>
        <footer style="text-align: center;">&copy; FauDroids 2015</footer>
    </body>
</html>
