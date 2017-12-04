package controllers

import javax.inject.Inject

import play.api.mvc.{AbstractController, ControllerComponents}

@Inject
class OptionsController @Inject() (cc: ControllerComponents) extends AbstractController(cc) {

  def options = Action {
    NoContent.withHeaders(
      "Access-Control-Allow-Origin" -> "*",
      "Access-Control-Allow-Methods" -> "GET, POST, OPTIONS, DELETE, PUT",
      "Access-Control-Max-Age" -> "3600",
      "Access-Control-Allow-Headers" -> "Origin, Content-Type, Accept, Authorization",
      "Access-Control-Allow-Credentials" -> "true"
    )
  }

}

