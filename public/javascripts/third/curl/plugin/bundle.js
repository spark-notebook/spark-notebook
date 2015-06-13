/**
 * This is a curl.js plugin for defining collections of resources that can be depended upon all at once.
 *
 * @author ryan.hendrickson
 */
define({
  load: function (resourceName, req, callback, config) {
    if (resourceName in config.bundles) {
      curl(config.bundles[resourceName]).then(callback, function (ex) { throw ex; });
    } else throw new Error('bundle ' + resourceName + ' is not defined');
  }
});