using System;

namespace FileShare
{
    public class Device
    {
        public string Id { get; set; }
        public string Name { get; set; }
        public string Address { get; set; }
        public int Port { get; set; }

        public override bool Equals(object obj)
        {
            if (obj is Device other)
            {
                return Id == other.Id;
            }
            return false;
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }
    }
} 