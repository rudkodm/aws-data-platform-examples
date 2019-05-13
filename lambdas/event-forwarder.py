import boto3
import json

region = 'us-east-1'
stream = 'customers_replication'
client = boto3.client('kinesis', region_name=region)


def handle(event, context):
    for record in event['Records']:
        payload = record['dynamodb']
        partition_key, partition_value = partition(payload['Keys'])
        timestamp = int(payload['ApproximateCreationDateTime'])
        payload = item(payload['NewImage'])
        payload['timestamp'] = timestamp
        put_response = client.put_record(
            StreamName=stream,
            Data=json.dumps(payload) + '\n',
            PartitionKey="%s::%s" % (partition_key, partition_value)
        )
        print("Request: '%s'" % str(payload))
        print("Response: '%s'" % str(put_response))


def partition(key_dif):
    k, v = first(key_dif)
    return k, value(v)


def item(item_dif):
    res = {}
    for k, v in item_dif.items():
        res[k] = value(v)
    return res


def value(val_dif):
    k, v = first(val_dif)
    if k == "S":
        return v
    elif k == 'N':
        return int(v)


def first(dict_obj):
    return list(dict_obj.items())[0]
