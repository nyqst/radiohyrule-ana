// Output Android Assets.jsx
// 2012 Todd Linkner
// License: none (public domain)
// v1.0
//
// This scrip is for Photoshop CS6. It outputs Android XHDPI, HDPI, MDPI, 
// and LDPI PNG assets from XHDPI source files. The resulting PNGs will be
// placed in sub-folders within your target folder.

/*
// BEGIN__HARVEST_EXCEPTION_ZSTRING
<javascriptresource>
<name>$$$/JavaScripts/AndroidAssetExporter/MenuAlt=Android Exporter</name>
<category>mobile</category>
</javascriptresource>
// END__HARVEST_EXCEPTION_ZSTRING
*/

// bring Photoshop into focus
#target photoshop

main();

function main() {

    // Ask user for input folder
    var inputFolder = Folder.selectDialog("Select input folder");
    if (inputFolder == null) throw "No input folder selected. Exting script.";

    // // Ask user for output folder
    // var outputFolder = Folder.selectDialog("Select output folder");
    // if (outputFolder == null) outputFolder = inputFolder;
    outputFolder = inputFolder + "/../res";

    // get all files in the input folder
    var fileList = inputFolder.getFiles("*.psd");

    // Make output folders
    var dirxxhdpi = Folder(outputFolder+"/drawable-xxhdpi");
    if(!dirxxhdpi.exists) dirxxhdpi.create();
    var dirxhdpi = Folder(outputFolder+"/drawable-xhdpi");
    if(!dirxhdpi.exists) dirxhdpi.create();
    var dirhdpi = Folder(outputFolder+"/drawable-hdpi");
    if(!dirhdpi.exists) dirhdpi.create();
    var dirmdpi = Folder(outputFolder+"/drawable-mdpi");
    if(!dirmdpi.exists) dirmdpi.create();

    // Open each file in turn
    for (var i = 0; i < fileList.length; i++) {
        // Open file
        open(fileList[i]);
        // Make XXHDPI
        resize(dirxxhdpi,480);
        // Make XHDPI
        resize(dirxhdpi,320);
        // Make HDPI
        resize(dirhdpi,240);
        // Make MDPI
        resize(dirmdpi,160);
        // Close and do not save changes
        app.activeDocument.close(SaveOptions.DONOTSAVECHANGES);
    }
    alert("" + fileList.length + " files exported.");
}

function resize(dir,resolution) {
    // Set target file name
    var fname = app.activeDocument.name.replace(/\.psd$/, '.png');

    // Set export options
    var opts, file;
    opts = new ExportOptionsSaveForWeb();
    opts.format = SaveDocumentType.PNG;
    opts.transparency = true;
    opts.interlaced = false;
    opts.optimized = true;
    opts.includeProfile = false;
    opts.PNG8 = false; 

    // Duplicate
    var tempfile = app.activeDocument.duplicate();

    // Resize
    tempfile.resizeImage(undefined,undefined,resolution);

    // Export
    file = new File(dir+"/"+fname);
    tempfile.exportDocument(file, ExportType.SAVEFORWEB, opts);
    tempfile.close(SaveOptions.DONOTSAVECHANGES);
}

