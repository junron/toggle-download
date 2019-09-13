package main.gui

import tornadofx.*

class GuiApp : App(AppView::class)

fun main() {
  launch<GuiApp>()
}