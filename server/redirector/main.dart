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
  print('Timer');
  var httpRequest = new HttpRequest();
  var time = new DateTime.now().millisecondsSinceEpoch;
  httpRequest
    ..open('GET', path+outputfile+'?t='+time.toString())
    ..onLoadEnd.listen((e) => outputRequestComplete(httpRequest))
    ..send('');
}

void outputRequestComplete(HttpRequest h){
  print('Log fetched');
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
  var time = new DateTime.now().millisecondsSinceEpoch;
  var httpRequest = new HttpRequest();
    httpRequest
      ..open('GET', path+statuscodefile+'?t='+time.toString())
      ..onLoadEnd.listen((e) => statusCodeRequestComplete(httpRequest))
      ..send('');
}

void statusCodeRequestComplete(HttpRequest h){
  print('Statuscode fetched'+h.responseText);
  if (h.responseText.contains('0')){
    print('Redirect');
    t.cancel();
    querySelector('#status').text = 'Finished!';
    window.location.replace(redirecturl);
  }
}