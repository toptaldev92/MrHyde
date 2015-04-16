<!doctype html>
<html>
    <head>
    <meta charset="utf-8">
    <title>MrHyde - a Jekyll scratchpad</title>
    </head>

    <body bgcolor="white" text="black">
        <p><strong>Available repositories:</strong></p>
        <table>
        %for row in rows:
            <tr>
                <td>{{row[0]}}</td><td><a href='{{row[1]}}'>View on GitHub</a></td>
            </tr>
        %end
        </table>
    </body>
</html>
