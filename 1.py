import re  

def contains(text, pattern):
    # 判断文本中是否包含指定的模式
    return bool(re.search(pattern, text))

def contains_structure_area_gt(text, threshold):
    # 判断文本中是否包含大于指定阈值的数字
    pattern = r'\d+'  # 匹配数字的正则表达式
    numbers = re.findall(pattern, text)  # 查找所有匹配的数字
    for number in numbers:
        if int(number) > threshold:  # 如果数字大于阈值，返回True
            return True
    return False

def analyze_image(decoded_response, count):
    # 分析图片内容，判断是否符合要求
    result = ""  # 初始化结果字符串
    if "设计说明" in decoded_response:
        # 如果图片包含“设计说明”字样，进行以下判断
        if (contains(decoded_response, ".*建筑设计防火规范2014.*") and contains(decoded_response, ".*2018.*")) or  (contains_structure_area_gt(decoded_response, 10000) and contains(decoded_response, ".*防火等级一级*")) or  (contains_structure_area_gt(decoded_response, 20000) and contains(decoded_response, ".*防火等级二级*")):
            # 如果满足以下任一条件，则认为符合要求
            print("目录" + str(count) + "合格" + "图片地址：")
            result = "目录" + str(count) + "合格" + "图片地址："
        else:
            # 如果不满足上述条件，分别判断不符合条件的情况
            if not (contains_structure_area_gt(decoded_response, 10000) and contains(decoded_response, ".*防火等级一级*")):
                result = "目录" + str(count) + "不包含防火等级一级和大于10000的数字 不合格" + "图片地址："
                print("目录" + str(count) + "不包含防火等级一级和大于10000的数字 不合格" + "图片地址：")
            elif not (contains(decoded_response, ".*建筑设计防火规范2014.*") and contains(decoded_response, ".*2018.*")):
                result = "目录" + str(count) + "不包含建筑设计防火规范2014和2018 不合格" + "图片地址："
                print("目录" + str(count) + "不包含建筑设计防火规范2014和2018 不合格" + "图片地址：")
            elif not (contains_structure_area_gt(decoded_response, 20000) and contains(decoded_response, ".*防火等级二级*")):
                result = "目录" + str(count) + "不包含防火等级二级和大于20000的数字 不合格" + "图片地址："
                print("目录" + str(count) + "不包含防火等级二级和大于20000的数字 不合格" + "图片地址：")
    else:
        # 如果图片不包含“设计说明”字样，则认为不符合要求
        result = "目录" + str(count) + "该图片不含消防二字，不合格" + "图片地址："
        print("目录" + str(count) + "该图片不含消防二字，不合格" + "图片地址：")
    
    return result
