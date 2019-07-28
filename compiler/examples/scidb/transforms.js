const Transform = require("../../src/Transform").Transform;

var dotsTransform = new Transform("",
     "",
     function (){},
     ["chrom", "pos", "ref", "alt", "rsid", "log10pvalue", "beta", "nobs", "note", "genes", "consequence", "xpos"],
     true);

var regionTransform = new Transform("",
    "",
    function (){},
    ["chrom","pos", "ref", "alt", "rsid", "log10pvalue", "beta", "genes", "consequence", "title", "desciprtion", "value_type", "notes", "xpos"],
    true);

var geneTransform = new Transform("",
    "",
    function () {},
    ["chrom", "start", "end", "name"],
    true);

var variantTransform = new Transform("",
    "",
    function (){},
    ["variant_id", "sub_field_id", "log10pvalue", "beta", "title", "description", "value_type", "notes", "pvalue_threshold", "variant"],
    true);

module.exports = {
    dotsTransform : dotsTransform,
    regionTransform : regionTransform,
    variantTransform : variantTransform,
    geneTransform : geneTransform
};
