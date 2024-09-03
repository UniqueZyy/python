if (value.contains("设计说明")) {
    TextUtil.contains(decodedResponse, ".*消防.*");
    //pdfFilePath = pictureAnalysis.textToPDF("该图片是消防图片");
    // 判断是否含有建筑设计防火规范2014和2018 或 判断是否含有防火等级一级和大于10000的数字    判断是否含有防火等级而级和大于20000的数字
    if ((TextUtil.contains(decodedResponse, ".*建筑设计防火规范2014.*") && TextUtil.contains(decodedResponse, ".*2018.*"))
            || (TextUtil.containsStructureAreaGt10000(decodedResponse) && TextUtil.contains(decodedResponse, ".*防火等级一级*"))
            || (TextUtil.containsStructureAreaGt20000(decodedResponse) && TextUtil.contains(decodedResponse, ".*防火等级二级*"))) {
        //textToWord("合格");
        System.out.println("目录" + count + "合格" + "图片地址：");
        result = "目录" + count + "合格" + "图片地址：";
    } else {
        if ((!(TextUtil.containsStructureAreaGt10000(decodedResponse) && TextUtil.contains(decodedResponse, ".*防火等级一级*")))) {
            //textToWord("不包含防火等级一级和大于10000的数字  不合格");
            result = "目录" + count + "不包含防火等级一级和大于10000的数字  不合格" + "图片地址：";
            System.out.println("目录" + count + "不包含防火等级一级和大于10000的数字  不合格" + "图片地址：");
        } else if (!(TextUtil.contains(decodedResponse, ".*建筑设计防火规范2014.*") && TextUtil.contains(decodedResponse, ".*2018.*"))) {
            //textToWord("不包含建筑设计防火规范2014和2018  不合格");
            result = "目录" + count + "不包含建筑设计防火规范2014和2018  不合格" + "图片地址：";
            System.out.println("目录" + count + "不包含建筑设计防火规范2014和2018  不合格" + "图片地址：");
        } else if (!(TextUtil.containsStructureAreaGt20000(decodedResponse) && TextUtil.contains(decodedResponse, ".*防火等级二级*"))) {
            //textToWord("不包含防火等级二级和大于20000的数字  不合格");
            result = "目录" + count + "不包含防火等级二级和大于20000的数字  不合格" + "图片地址：";
            System.out.println("目录" + count + "不包含防火等级二级和大于20000的数字  不合格" + "图片地址：");
        }
    }
} else {
    result = "目录" + count + "该图片不含消防二字，不合格" + "图片地址：";
    System.out.println("目录" + count + "该图片不含消防二字，不合格" + "图片地址：");
}