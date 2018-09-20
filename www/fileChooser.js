module.exports = {
    open: function (success, failure) {
        cordova.exec(success, failure, "FileChooser", "pick_file", []);
    },
    pickFile: function (success, fail, fileTypes) {
        cordova.exec(success, fail, "FileChooser", "pick_file", []);
    },
    pickFiles: function (success, fail, fileTypes) {
        cordova.exec(success, fail, "FileChooser", "pick_files", []);
    }
};
