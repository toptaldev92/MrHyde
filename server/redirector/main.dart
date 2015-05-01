import 'dart:html';
import 'dart:async';

String s;
var path = 'http://'+Uri.base.host.toString()+'/';
var outputfile = 'input.txt';
var statuscodefile = 'statuscode.txt';
var redirecturl = 'index.html';
Timer t;

void  main() {
  s = '';
  querySelector('#status').text = 'Getting Jekyll Output';
  t = new Timer.periodic(new Duration(seconds:1), timerTrigger);
}

void timerTrigger(Timer t){
  var httpRequest = new HttpRequest();
  httpRequest
    ..open('GET', path+outputfile)
    ..onLoadEnd.listen((e) => outputRequestComplete(httpRequest))
    ..send('');
}

void outputRequestComplete(HttpRequest h){
  var response = h.responseText.replaceFirst(s, '');
  s = s + response;
  List<String> list = response.split('\n');
  for (var line in list){
    if (line != ''){
      var element = new LIElement();
      element.text = line;
      querySelector('#output').children.add(element);
    }
  }
  var httpRequest = new HttpRequest();
    httpRequest
      ..open('GET', path+statuscodefile)
      ..onLoadEnd.listen((e) => statusCodeRequestComplete(httpRequest))
      ..send('');
}

void statusCodeRequestComplete(HttpRequest h){
  if (h.responseText.contains('0')){
    t.cancel();
    querySelector('#status').text = 'Finished!';
    window.location.assign(redirecturl);
  }
}