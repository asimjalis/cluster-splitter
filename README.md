# Cluster Splitter

Splits IP addresses into small training clusters.

## How to set up dev environment

Grab the latest source code using git.

    mkdir -p $HOME/git ; cd git
    git clone https://github.com/asimjalis/cluster-splitter.git
    cd cluster-splitter

Open the source code.

    gvim src-cljs/cluster-splitter.cljs 

Start a new terminal window to run cljsbuild. 

    cd $HOME/git/cluster-splitter
    lein cljsbuild auto dev
    
Start another terminal window to run the CLJS REPL.

    cd $HOME/git/cluster-splitter
    rlwrap lein trampoline cljsbuild repl-listen 
    
After you get the REPL prompt open Chrome and point it at
<http://localhost:9000/cluster-splitter.html>. 

Click *Cmd-Alt J* in Chrome to open up the JavaScript console.

In the console you should see `Welcome from REPL!`. 

Now in the REPL type,

    (js/alert "Hello world!!")

This will open up an alert dialog on the browser.

So things are looking good. We are all wired up and in business.

## How to modify the source and run unit tests

Every time you modify the source code and save it, the cljsbuild
process will pick up the changes and automatically recompile the
JavaScript. 

To pick up the fresh JavaScript file in the browser refresh the
browser.

To run the unit tests in the REPL type,

    (cluster_splitter.run-tests)

That's it.

## Why go to all this trouble

The reason we set up this elaborate environment is to that we have a
really short loop between development and execution.

You will test your changes in the REPL which will immediately affect
the browser. Also your changes are impacting the live browser so you
know if they work they are correct.

Once you are happy with your changes you can make them in the source
file, then pick up the file in the browser using the steps listed
above, and now again your changes are part of the runtime.

Once you get used to this it is really hard to go back to a
traditional development approach.

## License

Copyright (c) 2014 [Asim Jalis](http://twitter.com/asimjalis)

[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
