# eff

[![Build Status](https://travis-ci.org/atnos-org/eff-scalaz.png?branch=master)](https://travis-ci.org/atnon-org/eff-scalaz)

Extensible effects are an alternative to monad transformers for computing with effects in a functional way. 
This library is based on the "free-er" monad and an "open union" of effects described in 
Oleg Kiselyov in [Freer monads, more extensible effects](http://okmij.org/ftp/Haskell/extensible/more.pdf).

You can learn more in the User Guide:

 - [your first effects](http://atnos-org.github.io/eff-scalaz/org.atnos.site.Introduction.html)
 - [included effects: `Reader`, `Writer`, `Eval`, `State`,...](http://atnos-org.github.io/eff-scalaz/org.atnos.site.OutOfTheBox.html)
 - [using an open or a closed union of effects](http://atnos-org.github.io/eff-scalaz/org.atnos.site.OpenClosed.html)
 - [create your own effects](http://atnos-org.github.io/eff-scalaz/org.atnos.site.CreateEffects.html)
 - [working with different effect stacks](http://atnos-org.github.io/eff-scalaz/org.atnos.site.TransformStack.html)
 
## Installation

You add `eff-scalaz` as an sbt dependency:
```scala
libraryDependencies += "org.atnos" %% "eff-scalaz" % "1.3"
```

or download it from [here](https://oss.sonatype.org/content/repositories/releases/org/atnos/eff-scalaz_2.11/1.3/eff-scalaz_2.11-1.3.jar).
 
# Contributing
 
[eff-scalaz](https://github.com/atnos-org/eff-scalaz/) is a [Typelevel](http://typelevel.org) project. This means we embrace pure, typeful, functional programming,
and provide a safe and friendly environment for teaching, learning, and contributing as described in the [Typelevel Code of Conduct](http://typelevel.org/conduct.html). 

Feel free to open an issue if you notice a bug, have an idea for a feature, or have a question about the code. Pull requests are also gladly accepted. 
 
