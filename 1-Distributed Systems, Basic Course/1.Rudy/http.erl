-module(http).
-export([parse_request/1, ok/1, get/1]).

% parameter
%GET /index.html HTTP/1.1\r\nfoo 34\r\n\r\nHello

parse_request(R0) ->
  %GET /index.html HTTP/1.1\r\nfoo 34\r\n\r\nHello
  {Request, R1} = request_line(R0),
  %{{get, /index.html, v11}, \r\nfoo 34\r\n\r\nHello}
  {Headers, R2} = headers(R1),
  % ["foo 32"], "Hello"
  {Body, _ } = message_body(R2),
  %{ "Hello", []}
  {Request, Headers, Body}.
%skilum svo þessu


% pattern match
%"GET | /index.html HTTP/1.1\r\nfoo 34\r\n\r\nHello
request_line([$G, $E, $T, 32 |R0]) ->
  {URI, R1} = request_uri(R0),
  % {/index.html, HTTP/1.1\r\nfoo 34\r\n\r\nHello}
  {Ver, R2} = http_version(R1),
  % {HTTP/1.1, \r\nfoo 34\r\n\r\nHello}
  [13,10 | R3] = R2,

  {{get, URI, Ver}, R3}.

%/index.html HTTP/1.1\r\nfoo 34\r\n\r\nHello
% index.html -> C
% HTTP/1.1\r\nfoo 34\r\n\r\nHello -> R0
request_uri([32|R0]) ->
  {[],R0};
request_uri([C|R0]) ->
  {Rest, R1} = request_uri(R0),
  {[C|Rest], R1}.

% HTTP/1.1\r\nfoo 34\r\n\r\nHello
% HTTP/1.1 -> v11
% \r\nfoo 34\r\n\r\nHello -> R0
http_version([$H, $T, $T, $P, $/, $1, $., $1 |R0]) -> {v11, R0};
http_version([$H, $T, $T, $P, $/, $1, $., $0 |R0]) -> {v10, R0}.

% \r\nfoo 34\r\n\r\nHello
headers([13,10|R0]) ->
  {[], R0};
headers (R0) ->
  {Headers, R1} = header(R0),
  {Rest, R2} = headers(R1),
  {[Headers|Rest], R2}.

% \r\nfoo 34\r\n\r\nHello
header([13,10|R0]) ->
  {[], R0};
header([C|R0]) ->
  {Rest, R1} = header(R0),
  {[C|Rest], R1}.

ok(Body) ->
  "HTTP/1.1 200 OK\r\n" ++ "\r\n" ++ Body.

get(URI) ->
  "GET " ++ URI ++ " HTTP/1.1\r\n" ++ "\r\n".

message_body(R) ->
  {R,[]}.
