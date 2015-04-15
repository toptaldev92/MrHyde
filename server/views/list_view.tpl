%#template to generate a HTML table from a list or tuple or ...
%#one entry per row
<p>{{header}}</p>
<table>
%for row in rows:
  <tr>
    <td>{{row}}</td>
  </tr>
%end
</table>
