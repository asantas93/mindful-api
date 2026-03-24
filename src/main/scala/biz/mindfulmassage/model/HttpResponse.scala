package biz.mindfulmassage.model

case class HttpResponse[A](statusCode: Int, body: A, headers: Map[String, String])
