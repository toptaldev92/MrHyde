<!doctype html>
<html>
    <head>
    <meta charset="utf-8">
    <title>MrHyde - a Jekyll scratchpad</title>
    </head>

    <body bgcolor="white" text="black">
        <p><strong>{{header}}</strong></p>
        <table>
        %for row in rows:
            <tr><td>{{row}}</td></tr>
        %end
        </table>
    </body>
</html>
