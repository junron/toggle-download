package main.util

import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.paint.Color
import tornadofx.*

fun errorLabel(text: String) = object : Fragment() {
  override val root = this.label(text)
  {
    isWrapText = true
    style {
      textFill = Color.RED
    }
  }
}

fun Node.setChild(node: Node){
  this.getChildList()?.clear()
  this.getChildList()?.add(node)
}
